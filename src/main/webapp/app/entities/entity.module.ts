import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { Svn2GitMigrationModule } from './migration/migration.module';
import { Svn2GitMigrationHistoryModule } from './migration-history/migration-history.module';
/* jhipster-needle-add-entity-module-import - JHipster will add entity modules imports here */

@NgModule({
    // prettier-ignore
    imports: [
        Svn2GitMigrationModule,
        Svn2GitMigrationHistoryModule,
        /* jhipster-needle-add-entity-module - JHipster will add entity modules here */
    ],
    declarations: [],
    entryComponents: [],
    providers: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitEntityModule {}
