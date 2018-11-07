import { NgModule } from '@angular/core';

import { Svn2GitSharedLibsModule, FindLanguageFromKeyPipe, JhiAlertComponent, JhiAlertErrorComponent } from './';
import { SummaryCardComponent } from 'app/shared/summary/summary-card.component';
import { DetailsCardComponent } from 'app/shared/summary/summary-details.component';

@NgModule({
    imports: [Svn2GitSharedLibsModule],
    declarations: [FindLanguageFromKeyPipe, JhiAlertComponent, JhiAlertErrorComponent, SummaryCardComponent, DetailsCardComponent],
    exports: [
        Svn2GitSharedLibsModule,
        FindLanguageFromKeyPipe,
        JhiAlertComponent,
        JhiAlertErrorComponent,
        SummaryCardComponent,
        DetailsCardComponent
    ]
})
export class Svn2GitSharedCommonModule {}
