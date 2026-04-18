# Contributing to Analyse

Thanks for taking the time to help improve Analyse. This page is the short version of how we work.

## Before you open a PR

> [!IMPORTANT]
> By submitting any contribution (pull request, patch, issue reproduction, or snippet) to this repository, you assign the intellectual property rights of that contribution to **VertCode Development E.E.** under the terms of [Section 3 of the LICENSE](LICENSE). Please make sure you own what you submit and are OK with that assignment before opening a PR.

Also:

- Check the [issue tracker](https://github.com/Analyse-net/analyse-java/issues) first. If an issue doesn't exist for what you want to change, open one. Large unsolicited PRs are unlikely to be merged without prior discussion.
- Keep PRs focused. One logical change per PR. Refactors and feature work in separate PRs please.
- Follow the code style below.

## Building locally

Requirements:

- JDK 21 or newer
- Git

```bash
git clone https://github.com/Analyse-net/analyse-java
cd analyse-java
./gradlew build
```

Jar files land in each `modules/*/build/libs/` directory.

To build everything and bundle the jars into a release zip:

```bash
./scripts/release.sh
```

## Running against your own server

The fastest inner loop is:

1. Run `./gradlew :modules:spigot:build`.
2. Copy `modules/spigot/build/libs/analyse-spigot-<version>.jar` into your test server's `plugins/` folder.
3. Restart the server.

For testing against a non-production Analyse API, set `development: true` in your config and the plugin will route requests to the staging environment.

## Code style

The full style guide lives in [`.cursor/rules/java-style.mdc`](.cursor/rules/java-style.mdc). The highlights:

- **Always use braces** for `if` statements, even single-line ones.
- **Add an empty line** after an `if` block before the next statement.
- **Do NOT** add an empty line between a variable declaration and its related `if` check.
- **Extract method calls into variables** before passing them into other calls.
- **Javadoc** on every method (public, private, or protected) except `@EventHandler` listeners.
- **Use `String.format`** for logger messages with variables, not string concatenation.
- **No comment banners** (no `// ==== SECTION ====`).
- **Singletons:** one `getInstance()` as a field initializer is OK; two or more calls means you should pass the instance through the constructor.
- **Lombok:** `@Getter` is fine; avoid class-level `@Setter`, write setters by hand when you need them.

Formatting is two spaces for indentation, UTF-8, LF line endings.

## Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description
```

Types we use: `feat`, `fix`, `refactor`, `docs`, `style`, `test`, `chore`.

Good examples:

```
feat(Spigot): add bedrock prefix detection to session manager
fix(Velocity): fall back to first backend when defaultServer is null
refactor(sdk): simplify heartbeat retry logic
docs(README): clarify supported server software
chore(deps): bump okhttp to 4.12.1
```

Write descriptions in the imperative, no trailing period, start with lowercase. More examples and bad examples live in [`.cursor/rules/commit-101.mdc`](.cursor/rules/commit-101.mdc).

## PR checklist

Before you click "ready for review":

- [ ] The code compiles (`./gradlew build`).
- [ ] The code follows the style guide.
- [ ] You updated the docs under `docs/` if your change affects user-visible behavior.
- [ ] You added an entry to `CHANGELOG.md` under **Unreleased**.
- [ ] Your commits follow Conventional Commits.
- [ ] The PR description links the issue it closes (`Closes #123`).

## Reporting bugs

Open a [bug report](https://github.com/Analyse-net/analyse-java/issues/new?template=bug_report.md). Include:

- Server software and version (`/version` output is perfect)
- Analyse plugin version
- What you expected vs. what happened
- A minimal reproduction
- Relevant log lines (wrap them in a fenced code block, please don't attach a 50 MB log)

## Reporting security issues

Please do NOT open a public issue for security vulnerabilities. See [SECURITY.md](SECURITY.md) for the right channel.

## Questions

For usage questions, use [GitHub Discussions](https://github.com/Analyse-net/analyse-java/discussions) or the [Analyse Discord](https://analyse.net). Issues are for bugs and feature requests only.

Thanks again.
