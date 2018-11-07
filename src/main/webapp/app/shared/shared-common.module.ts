import { NgModule } from '@angular/core';

import { Svn2GitSharedLibsModule, FindLanguageFromKeyPipe, JhiAlertComponent, JhiAlertErrorComponent } from './';
import { SummaryCardComponent } from 'app/shared/summary/summary-card.component';

@NgModule({
    imports: [Svn2GitSharedLibsModule],
    declarations: [FindLanguageFromKeyPipe, JhiAlertComponent, JhiAlertErrorComponent, SummaryCardComponent],
    exports: [Svn2GitSharedLibsModule, FindLanguageFromKeyPipe, JhiAlertComponent, JhiAlertErrorComponent, SummaryCardComponent]
})
export class Svn2GitSharedCommonModule {}
