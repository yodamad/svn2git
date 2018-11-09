import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiAlertService } from 'ng-jhipster';

import { IStaticMapping } from 'app/shared/model/static-mapping.model';
import { Principal } from 'app/core';
import { StaticMappingService } from './static-mapping.service';

@Component({
    selector: 'jhi-static-mapping',
    templateUrl: './static-mapping.component.html'
})
export class StaticMappingComponent implements OnInit, OnDestroy {
    staticMappings: IStaticMapping[];
    currentAccount: any;
    eventSubscriber: Subscription;

    constructor(
        private staticMappingService: StaticMappingService,
        private jhiAlertService: JhiAlertService,
        private eventManager: JhiEventManager,
        private principal: Principal
    ) {}

    loadAll() {
        this.staticMappingService.query().subscribe(
            (res: HttpResponse<IStaticMapping[]>) => {
                this.staticMappings = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    ngOnInit() {
        this.loadAll();
        this.principal.identity().then(account => {
            this.currentAccount = account;
        });
        this.registerChangeInStaticMappings();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: IStaticMapping) {
        return item.id;
    }

    registerChangeInStaticMappings() {
        this.eventSubscriber = this.eventManager.subscribe('staticMappingListModification', response => this.loadAll());
    }

    private onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
