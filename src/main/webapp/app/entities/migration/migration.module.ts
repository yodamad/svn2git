import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { Svn2GitSharedModule } from 'app/shared';
import {
    MigrationComponent,
    MigrationDetailComponent,
    MigrationUpdateComponent,
    MigrationDeletePopupComponent,
    MigrationDeleteDialogComponent,
    migrationRoute,
    migrationPopupRoute
} from './';

const ENTITY_STATES = [...migrationRoute, ...migrationPopupRoute];

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        MigrationComponent,
        MigrationDetailComponent,
        MigrationUpdateComponent,
        MigrationDeleteDialogComponent,
        MigrationDeletePopupComponent
    ],
    entryComponents: [MigrationComponent, MigrationUpdateComponent, MigrationDeleteDialogComponent, MigrationDeletePopupComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitMigrationModule {}
