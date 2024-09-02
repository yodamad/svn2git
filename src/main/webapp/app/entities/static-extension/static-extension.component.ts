import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiAlertService } from 'ng-jhipster';
import { IStaticExtension } from 'app/shared/model/static-extension.model';
import { StaticExtensionService } from './static-extension.service';

@Component({
    selector: 'jhi-static-extension',
    templateUrl: './static-extension.component.html'
})
export class StaticExtensionComponent implements OnInit, OnDestroy {
    staticExtensions: IStaticExtension[];
    currentAccount: any;
    eventSubscriber: Subscription;

    constructor(
        private staticExtensionService: StaticExtensionService,
        private jhiAlertService: JhiAlertService,
        private eventManager: JhiEventManager
    ) {}

    loadAll() {
        this.staticExtensionService.query().subscribe(
            (res: HttpResponse<IStaticExtension[]>) => {
                this.staticExtensions = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    ngOnInit() {
        this.loadAll();
        this.registerChangeInStaticExtensions();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: IStaticExtension) {
        return item.id;
    }

    registerChangeInStaticExtensions() {
        this.eventSubscriber = this.eventManager.subscribe('staticExtensionListModification', response => this.loadAll());
    }

    private onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
