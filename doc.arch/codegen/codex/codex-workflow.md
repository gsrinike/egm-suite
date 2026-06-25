# Codex Workflow

## Before Changing Files

1. Read the relevant code and documentation first.
2. Read the files in `doc.arch/codegen/codex` for repo-specific rules.
3. Check `git status --short` and preserve unrelated user changes.
4. Prefer `rg` and `rg --files` for searching.

## During Implementation

- Keep edits scoped to the user request.
- Use existing module patterns before inventing new abstractions.
- Use `apply_patch` for manual edits.
- Do not use destructive Git commands unless the user explicitly asks and the intent is clear.
- Reintroduced domain modules should use the shared `com.*` capabilities rather than duplicating configuration, mapping, infrastructure, authentication, or vault logic.
- Keep configuration YAML-based.
- Keep shared modules domain-neutral.

## Documentation Updates

When behavior, module shape, dependency rules, or deployment expectations change, update the relevant docs:

- root `README.md`
- module `README.md`
- `doc.arch/*.md`
- `doc.arch/codegen/codex/*.md` when Codex guidance changes

## Verification

For code changes, run the remaining reactor tests:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true test
```

For documentation-only changes, run targeted scans for stale references when relevant.

## Before Commit Or Push

- Ensure `git status --short` contains only intentional source/documentation changes.
- Ensure no `work/m2` files are staged.
- Prefer one clean commit for a coherent requested change.
- If a push fails due to large pack or HTTP transport issues, inspect for accidentally staged/generated files before retrying.
