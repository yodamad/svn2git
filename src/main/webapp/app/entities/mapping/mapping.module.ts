import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { Svn2GitSharedModule } from 'app/shared';
import {
    MappingComponent,
    MappingDetailComponent,
    MappingUpdateComponent,
    MappingDeletePopupComponent,
    MappingDeleteDialogComponent,
    mappingRoute,
    mappingPopupRoute
} from './';

const ENTITY_STATES = [...mappingRoute, ...mappingPopupRoute];

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        MappingComponent,
        MappingDetailComponent,
        MappingUpdateComponent,
        MappingDeleteDialogComponent,
        MappingDeletePopupComponent
    ],
    entryComponents: [MappingComponent, MappingUpdateComponent, MappingDeleteDialogComponent, MappingDeletePopupComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitMappingModule {}
