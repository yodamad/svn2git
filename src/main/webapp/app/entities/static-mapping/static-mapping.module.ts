import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { Svn2GitSharedModule } from 'app/shared';
import {
    StaticMappingComponent,
    StaticMappingDetailComponent,
    StaticMappingUpdateComponent,
    StaticMappingDeletePopupComponent,
    StaticMappingDeleteDialogComponent,
    staticMappingRoute,
    staticMappingPopupRoute
} from './';

const ENTITY_STATES = [...staticMappingRoute, ...staticMappingPopupRoute];

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        StaticMappingComponent,
        StaticMappingDetailComponent,
        StaticMappingUpdateComponent,
        StaticMappingDeleteDialogComponent,
        StaticMappingDeletePopupComponent
    ],
    entryComponents: [
        StaticMappingComponent,
        StaticMappingUpdateComponent,
        StaticMappingDeleteDialogComponent,
        StaticMappingDeletePopupComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitStaticMappingModule {}
