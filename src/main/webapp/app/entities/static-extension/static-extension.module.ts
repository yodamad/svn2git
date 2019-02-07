import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { Svn2GitSharedModule } from 'app/shared';
import {
    StaticExtensionComponent,
    StaticExtensionDetailComponent,
    StaticExtensionUpdateComponent,
    StaticExtensionDeletePopupComponent,
    StaticExtensionDeleteDialogComponent,
    staticExtensionRoute,
    staticExtensionPopupRoute
} from './';

const ENTITY_STATES = [...staticExtensionRoute, ...staticExtensionPopupRoute];

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        StaticExtensionComponent,
        StaticExtensionDetailComponent,
        StaticExtensionUpdateComponent,
        StaticExtensionDeleteDialogComponent,
        StaticExtensionDeletePopupComponent
    ],
    entryComponents: [
        StaticExtensionComponent,
        StaticExtensionUpdateComponent,
        StaticExtensionDeleteDialogComponent,
        StaticExtensionDeletePopupComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitStaticExtensionModule {}
