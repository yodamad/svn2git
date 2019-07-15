import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';

import { Svn2GitMigrationModule } from './migration/migration.module';
import { Svn2GitMigrationHistoryModule } from './migration-history/migration-history.module';
import { Svn2GitMappingModule } from './mapping/mapping.module';
import { Svn2GitStaticMappingModule } from './static-mapping/static-mapping.module';
import { Svn2GitStaticExtensionModule } from './static-extension/static-extension.module';
import { Svn2GitMigrationRemovedFileModule } from './migration-removed-file/migration-removed-file.module';

/* jhipster-needle-add-entity-module-import - JHipster will add entity modules imports here */

@NgModule({
    // prettier-ignore
    imports: [
        Svn2GitMigrationModule,
        Svn2GitMigrationHistoryModule,
        Svn2GitMappingModule,
        Svn2GitStaticMappingModule,
        Svn2GitStaticExtensionModule,
        Svn2GitMigrationRemovedFileModule,
        /* jhipster-needle-add-entity-module - JHipster will add entity modules here */
    ],
    declarations: [],
    entryComponents: [],
    providers: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitEntityModule {}
