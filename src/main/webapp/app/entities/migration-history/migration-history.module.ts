import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { Svn2GitSharedModule } from 'app/shared';
import {
    MigrationHistoryComponent,
    MigrationHistoryDetailComponent,
    MigrationHistoryUpdateComponent,
    MigrationHistoryDeletePopupComponent,
    MigrationHistoryDeleteDialogComponent,
    migrationHistoryRoute,
    migrationHistoryPopupRoute
} from './';

const ENTITY_STATES = [...migrationHistoryRoute, ...migrationHistoryPopupRoute];

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        MigrationHistoryComponent,
        MigrationHistoryDetailComponent,
        MigrationHistoryUpdateComponent,
        MigrationHistoryDeleteDialogComponent,
        MigrationHistoryDeletePopupComponent
    ],
    entryComponents: [
        MigrationHistoryComponent,
        MigrationHistoryUpdateComponent,
        MigrationHistoryDeleteDialogComponent,
        MigrationHistoryDeletePopupComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitMigrationHistoryModule {}
