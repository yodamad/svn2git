import { NgModule } from '@angular/core';

import { Svn2GitSharedLibsModule, FindLanguageFromKeyPipe, JhiAlertComponent, JhiAlertErrorComponent } from './';
import { SummaryCardComponent } from 'app/shared/summary/summary-card.component';
import { DetailsCardComponent } from 'app/shared/summary/summary-details.component';
import { SummaryMappingsComponent } from 'app/shared/summary/summary-mappings.component';

@NgModule({
    imports: [Svn2GitSharedLibsModule],
    declarations: [
        FindLanguageFromKeyPipe,
        JhiAlertComponent,
        JhiAlertErrorComponent,
        SummaryCardComponent,
        DetailsCardComponent,
        SummaryMappingsComponent
    ],
    exports: [
        Svn2GitSharedLibsModule,
        FindLanguageFromKeyPipe,
        JhiAlertComponent,
        JhiAlertErrorComponent,
        SummaryCardComponent,
        DetailsCardComponent,
        SummaryMappingsComponent
    ]
})
export class Svn2GitSharedCommonModule {}
