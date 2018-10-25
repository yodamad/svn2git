import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiAlertService } from 'ng-jhipster';

import { IMigrationHistory } from 'app/shared/model/migration-history.model';
import { Principal } from 'app/core';
import { MigrationHistoryService } from './migration-history.service';

@Component({
    selector: 'jhi-migration-history',
    templateUrl: './migration-history.component.html'
})
export class MigrationHistoryComponent implements OnInit, OnDestroy {
    migrationHistories: IMigrationHistory[];
    currentAccount: any;
    eventSubscriber: Subscription;

    constructor(
        private migrationHistoryService: MigrationHistoryService,
        private jhiAlertService: JhiAlertService,
        private eventManager: JhiEventManager,
        private principal: Principal
    ) {}

    loadAll() {
        this.migrationHistoryService.query().subscribe(
            (res: HttpResponse<IMigrationHistory[]>) => {
                this.migrationHistories = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    ngOnInit() {
        this.loadAll();
        this.principal.identity().then(account => {
            this.currentAccount = account;
        });
        this.registerChangeInMigrationHistories();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: IMigrationHistory) {
        return item.id;
    }

    registerChangeInMigrationHistories() {
        this.eventSubscriber = this.eventManager.subscribe('migrationHistoryListModification', response => this.loadAll());
    }

    private onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
