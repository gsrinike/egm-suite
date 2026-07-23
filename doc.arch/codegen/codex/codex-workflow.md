# Codex Workflow

## Before Editing

1. Read the relevant code and documentation.
2. Read this folder for repository rules.
3. Check `git status --short`.
4. Preserve unrelated user changes.
5. Prefer `rg` and `rg --files` for discovery.

## While Editing

- Keep changes scoped to the request.
- Follow existing module boundaries and naming.
- Use `apply_patch` for manual edits.
- Keep configuration YAML-based.
- Keep shared `com.*` modules domain-neutral.
- Do not use destructive Git commands unless explicitly requested.

## Documentation

When behavior or module shape changes, update the relevant module README and
the matching `doc.arch` design document. Write documentation as current state
unless the user explicitly asks for a migration history.

## Verification

Run the smallest meaningful verification command for the touched modules. For
documentation-only changes, use reference scans and Markdown sanity checks.
