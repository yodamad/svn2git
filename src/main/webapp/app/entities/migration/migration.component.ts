import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiAlertService, JhiEventManager, JhiParseLinks } from 'ng-jhipster';
import { IMigration, StatusEnum } from 'app/shared/model/migration.model';
import { ITEMS_PER_PAGE } from 'app/shared';
import { MigrationService } from './migration.service';

@Component({
    selector: 'jhi-migration',
    templateUrl: './migration.component.html',
    styleUrls: ['./migration.component.css']
})
export class MigrationComponent implements OnInit, OnDestroy {
    migrations: IMigration[];
    currentAccount: any;
    eventSubscriber: Subscription;
    itemsPerPage: number;
    links: any;
    page: any;
    predicate: any;
    queryCount: any;
    reverse: any;
    totalItems: number;

    constructor(
        private migrationService: MigrationService,
        private jhiAlertService: JhiAlertService,
        private eventManager: JhiEventManager,
        private parseLinks: JhiParseLinks
    ) {
        this.migrations = [];
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.page = 0;
        this.links = {
            last: 0
        };
        this.predicate = 'id';
        this.reverse = true;
    }

    loadAll() {
        this.migrationService
            .query({
                page: this.page,
                size: this.itemsPerPage,
                sort: this.sort()
            })
            .subscribe(
                (res: HttpResponse<IMigration[]>) => this.paginateMigrations(res.body, res.headers),
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    reset() {
        this.page = 0;
        this.migrations = [];
        this.loadAll();
    }

    loadPage(page) {
        this.page = page;
        this.loadAll();
    }

    ngOnInit() {
        this.loadAll();
        this.registerChangeInMigrations();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: IMigration) {
        return item.id;
    }

    registerChangeInMigrations() {
        this.eventSubscriber = this.eventManager.subscribe('migrationListModification', response => this.reset());
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    /**
     * Add class according to status
     * @param status
     */
    cssClass(status: StatusEnum) {
        if (status === StatusEnum.DONE) {
            return 'cell-ok';
        }
        if (status === StatusEnum.FAILED) {
            return 'cell-ko';
        }
        if (status === StatusEnum.RUNNING) {
            return 'badge-primary';
        }
        if (status === StatusEnum.IGNORED) {
            return 'cell-ignored';
        }
        if (status === StatusEnum.DONE_WITH_WARNINGS) {
            return 'cell-warning';
        }
        if (status === StatusEnum.WAITING) {
            return 'cell-waiting';
        }
    }

    private paginateMigrations(data: IMigration[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        for (let i = 0; i < data.length; i++) {
            this.migrations.push(data[i]);
        }
    }

    private onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
