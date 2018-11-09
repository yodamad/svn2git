import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { Svn2GitSharedModule } from 'app/shared';
import { MigrationHistoryComponent, MigrationHistoryDetailComponent, migrationHistoryRoute } from './';

const ENTITY_STATES = [...migrationHistoryRoute];

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [MigrationHistoryComponent, MigrationHistoryDetailComponent],
    entryComponents: [MigrationHistoryComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitMigrationHistoryModule {}
