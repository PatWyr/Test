package com.gitlab.mvysny.jdbiorm.quirks;

import com.gitlab.mvysny.jdbiorm.JdbiOrm;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.spi.JdbiPlugin;

import java.sql.SQLException;

/**
 * Auto-detects {@link Quirks} which needs to be activated for certain databases.
 * If {@link JdbiOrm#quirks} is set, then the whole auto-detection mechanism
 * is disabled.
 * @author mavi
 */
public class DatabaseQuirksDetectorJdbiPlugin extends JdbiPlugin.Singleton {

    @Override
    public Handle customizeHandle(Handle handle) throws SQLException {
        final DatabaseVariant variant = DatabaseVariant.detect(handle.getConnection());
        final Quirks quirks = JdbiOrm.quirks != null ? JdbiOrm.quirks : variant.getQuirks();
        final Quirks.Holder holder = handle.getConfig(Quirks.Holder.class);
        holder.quirks = quirks;
        holder.variant = variant;
        quirks.configure(handle);
        return handle;
    }
}
