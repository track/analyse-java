import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

version = "2024.12"

project {
    vcsRoot(AnalyseVcs)
    buildType(Build)
}

object AnalyseVcs : GitVcsRoot({
    name = "Analyse Server Plugins"
    url = "https://github.com/VCD/Analyse.git"
    branch = "refs/heads/main"
    branchSpec = "+:refs/heads/*"
})

object Build : BuildType({
    name = "Build"
    description = "Build all Analyse plugins"

    artifactRules = """
        paper/build/libs/analyse-paper-*.jar
        velocity/build/libs/analyse-velocity-*.jar
        bungeecord/build/libs/analyse-bungeecord-*.jar
    """.trimIndent()

    vcs {
        root(AnalyseVcs)
    }

    steps {
        gradle {
            name = "Build"
            tasks = "clean build"
            gradleWrapperPath = ""
        }
    }

    triggers {
        vcs {
            branchFilter = "+:*"
        }
    }
})

