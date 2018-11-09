import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { Svn2GitSharedModule } from 'app/shared';
import { MappingComponent, MappingDetailComponent, mappingRoute } from './';

const ENTITY_STATES = [...mappingRoute];

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [MappingComponent, MappingDetailComponent],
    entryComponents: [MappingComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitMappingModule {}
