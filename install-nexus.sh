#!/bin/bash

# =============================================================================
# Nexus Repository Manager Installation Script for Ubuntu
# Safe for systems running Pterodactyl - NO apt upgrade commands
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

NEXUS_VERSION="3.72.0-04"
NEXUS_DOWNLOAD_URL="https://download.sonatype.com/nexus/3/nexus-${NEXUS_VERSION}-unix.tar.gz"
NEXUS_HOME="/opt/nexus"
NEXUS_DATA="/opt/sonatype-work"
NEXUS_USER="nexus"

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Nexus Repository Manager Installer  ${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo -e "${RED}This script must be run as root (use sudo)${NC}"
   exit 1
fi

echo -e "${YELLOW}[1/7] Installing required packages (Java 11)...${NC}"
# Only install specific packages, no upgrade
apt-get update -qq
apt-get install -y --no-upgrade openjdk-11-jdk wget curl

# Verify Java installation
java -version 2>&1 | head -n 1
echo -e "${GREEN}Java installed successfully${NC}"

echo -e "${YELLOW}[2/7] Creating nexus user...${NC}"
if id "$NEXUS_USER" &>/dev/null; then
    echo -e "${GREEN}User '$NEXUS_USER' already exists${NC}"
else
    useradd -r -m -U -d "$NEXUS_HOME" -s /bin/false "$NEXUS_USER"
    echo -e "${GREEN}User '$NEXUS_USER' created${NC}"
fi

echo -e "${YELLOW}[3/7] Downloading Nexus ${NEXUS_VERSION}...${NC}"
cd /tmp
if [[ -f "nexus-${NEXUS_VERSION}-unix.tar.gz" ]]; then
    echo "Nexus archive already downloaded, skipping..."
else
    wget -q --show-progress "$NEXUS_DOWNLOAD_URL" -O "nexus-${NEXUS_VERSION}-unix.tar.gz"
fi

echo -e "${YELLOW}[4/7] Extracting Nexus...${NC}"
# Clean up old installation if exists
if [[ -d "$NEXUS_HOME" ]]; then
    echo -e "${YELLOW}Backing up existing installation...${NC}"
    mv "$NEXUS_HOME" "${NEXUS_HOME}.backup.$(date +%Y%m%d%H%M%S)"
fi

mkdir -p "$NEXUS_HOME"
tar -xzf "nexus-${NEXUS_VERSION}-unix.tar.gz" -C /opt

# Rename extracted folder
mv /opt/nexus-${NEXUS_VERSION}/* "$NEXUS_HOME"/
rm -rf /opt/nexus-${NEXUS_VERSION}

# Create data directory if not exists
mkdir -p "$NEXUS_DATA"

echo -e "${YELLOW}[5/7] Configuring Nexus...${NC}"
# Set run_as_user
echo "run_as_user=\"$NEXUS_USER\"" > "$NEXUS_HOME/bin/nexus.rc"

# Configure JVM options (adjust memory based on your server)
cat > "$NEXUS_HOME/bin/nexus.vmoptions" << 'EOF'
-Xms1024m
-Xmx1024m
-XX:MaxDirectMemorySize=1024m
-XX:+UnlockDiagnosticVMOptions
-XX:+LogVMOutput
-XX:LogFile=../sonatype-work/nexus3/log/jvm.log
-XX:-OmitStackTraceInFastThrow
-Djava.net.preferIPv4Stack=true
-Dkaraf.home=.
-Dkaraf.base=.
-Dkaraf.etc=etc/karaf
-Djava.util.logging.config.file=etc/karaf/java.util.logging.properties
-Dkaraf.data=../sonatype-work/nexus3
-Dkaraf.log=../sonatype-work/nexus3/log
-Djava.io.tmpdir=../sonatype-work/nexus3/tmp
EOF

# Set ownership
chown -R "$NEXUS_USER":"$NEXUS_USER" "$NEXUS_HOME"
chown -R "$NEXUS_USER":"$NEXUS_USER" "$NEXUS_DATA"

echo -e "${YELLOW}[6/7] Creating systemd service...${NC}"
cat > /etc/systemd/system/nexus.service << EOF
[Unit]
Description=Nexus Repository Manager
After=network.target

[Service]
Type=forking
LimitNOFILE=65536
ExecStart=${NEXUS_HOME}/bin/nexus start
ExecStop=${NEXUS_HOME}/bin/nexus stop
User=${NEXUS_USER}
Group=${NEXUS_USER}
Restart=on-abort
TimeoutSec=600

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd
systemctl daemon-reload

echo -e "${YELLOW}[7/7] Starting Nexus service...${NC}"
systemctl enable nexus
systemctl start nexus

# Wait for Nexus to start
echo -e "${YELLOW}Waiting for Nexus to start (this may take 1-2 minutes)...${NC}"
sleep 30

# Check if Nexus is running
if systemctl is-active --quiet nexus; then
    echo -e "${GREEN}Nexus is running!${NC}"
else
    echo -e "${RED}Nexus failed to start. Check logs with: journalctl -u nexus -f${NC}"
    exit 1
fi

# Get initial admin password
ADMIN_PASSWORD_FILE="${NEXUS_DATA}/nexus3/admin.password"
echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Installation Complete!              ${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo -e "Nexus URL: ${GREEN}http://YOUR_SERVER_IP:8081${NC}"
echo -e "Default username: ${GREEN}admin${NC}"
echo ""

if [[ -f "$ADMIN_PASSWORD_FILE" ]]; then
    echo -e "Initial admin password: ${GREEN}$(cat $ADMIN_PASSWORD_FILE)${NC}"
    echo -e "${YELLOW}(You will be prompted to change this on first login)${NC}"
else
    echo -e "${YELLOW}Admin password file not found yet. Wait a minute and check:${NC}"
    echo -e "${YELLOW}cat ${ADMIN_PASSWORD_FILE}${NC}"
fi

echo ""
echo -e "Useful commands:"
echo -e "  Check status:  ${GREEN}systemctl status nexus${NC}"
echo -e "  View logs:     ${GREEN}journalctl -u nexus -f${NC}"
echo -e "  Restart:       ${GREEN}systemctl restart nexus${NC}"
echo -e "  Stop:          ${GREEN}systemctl stop nexus${NC}"
echo ""

# Clean up
rm -f /tmp/nexus-${NEXUS_VERSION}-unix.tar.gz

echo -e "${GREEN}Done!${NC}"
