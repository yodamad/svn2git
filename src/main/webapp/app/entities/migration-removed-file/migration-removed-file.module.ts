import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { Svn2GitSharedModule } from 'app/shared';
import {
    MigrationRemovedFileComponent,
    MigrationRemovedFileDetailComponent,
    MigrationRemovedFileUpdateComponent,
    MigrationRemovedFileDeletePopupComponent,
    MigrationRemovedFileDeleteDialogComponent,
    migrationRemovedFileRoute,
    migrationRemovedFilePopupRoute
} from './';

const ENTITY_STATES = [...migrationRemovedFileRoute, ...migrationRemovedFilePopupRoute];

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        MigrationRemovedFileComponent,
        MigrationRemovedFileDetailComponent,
        MigrationRemovedFileUpdateComponent,
        MigrationRemovedFileDeleteDialogComponent,
        MigrationRemovedFileDeletePopupComponent
    ],
    entryComponents: [
        MigrationRemovedFileComponent,
        MigrationRemovedFileUpdateComponent,
        MigrationRemovedFileDeleteDialogComponent,
        MigrationRemovedFileDeletePopupComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitMigrationRemovedFileModule {}
