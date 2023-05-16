import './vendor.ts';

import { NgModule, Injector, APP_INITIALIZER } from '@angular/core';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { NgbDatepickerConfig } from '@ng-bootstrap/ng-bootstrap';
import { NgxWebstorageModule, LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { JhiEventManager } from 'ng-jhipster';
import { ErrorHandlerInterceptor } from './blocks/interceptor/errorhandler.interceptor';
import { NotificationInterceptor } from './blocks/interceptor/notification.interceptor';
import { Svn2GitSharedModule } from 'app/shared';
import { Svn2GitCoreModule } from 'app/core';
import { Svn2GitAppRoutingModule } from './app-routing.module';
import { Svn2GitHomeModule } from './home/home.module';
import { Svn2GitEntityModule } from './entities/entity.module';
import * as moment from 'moment';
// jhipster-needle-angular-add-module-import JHipster will add new module here
import { JhiMainComponent, NavbarComponent, FooterComponent, PageRibbonComponent, ActiveMenuDirective, ErrorComponent } from './layouts';
import { Svn2GitMigrationModule } from 'app/migration/migation-process.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { ConfigurationService } from 'app/shared/service/configuration-service';

export function configInit(configService: ConfigurationService) {
    return () => configService.init();
}

@NgModule({
    imports: [
        Svn2GitAppRoutingModule,
        NgxWebstorageModule.forRoot({ prefix: 'jhi', separator: '-' }),
        Svn2GitSharedModule,
        Svn2GitCoreModule,
        Svn2GitHomeModule,
        Svn2GitEntityModule,
        Svn2GitMigrationModule,
        BrowserAnimationsModule,
        BrowserModule
        // jhipster-needle-angular-add-module JHipster will add new module here
    ],
    declarations: [JhiMainComponent, NavbarComponent, ErrorComponent, PageRibbonComponent, ActiveMenuDirective, FooterComponent],
    providers: [
        {
            provide: HTTP_INTERCEPTORS,
            useClass: ErrorHandlerInterceptor,
            multi: true,
            deps: [JhiEventManager]
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: NotificationInterceptor,
            multi: true,
            deps: [Injector]
        },
        {
            provide: APP_INITIALIZER,
            useFactory: configInit,
            multi: true,
            deps: [ConfigurationService]
        }
    ],
    bootstrap: [JhiMainComponent]
})
export class Svn2GitAppModule {
    constructor(private dpConfig: NgbDatepickerConfig) {
        this.dpConfig.minDate = { year: moment().year() - 100, month: 1, day: 1 };
    }
}
