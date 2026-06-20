# com.env

`com.env` exposes `EnvironmentResolverService`, the suite runtime environment resolver. It reads `env` from JVM system properties first, then `ENV` from operating system environment variables, and defaults to `local` when neither is set.

The resolved value is normalized to lower case and is used by `com.app.config` to load files from environment folders, such as `local/srv.cgm.importer-application.xml`.
