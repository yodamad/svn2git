import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { JhiAlertService } from 'ng-jhipster';

import { IMigrationHistory } from 'app/shared/model/migration-history.model';
import { MigrationHistoryService } from './migration-history.service';
import { IMigration } from 'app/shared/model/migration.model';
import { MigrationService } from 'app/entities/migration';

@Component({
    selector: 'jhi-migration-history-update',
    templateUrl: './migration-history-update.component.html'
})
export class MigrationHistoryUpdateComponent implements OnInit {
    migrationHistory: IMigrationHistory;
    isSaving: boolean;

    migrations: IMigration[];
    date: string;

    constructor(
        private jhiAlertService: JhiAlertService,
        private migrationHistoryService: MigrationHistoryService,
        private migrationService: MigrationService,
        private activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ migrationHistory }) => {
            this.migrationHistory = migrationHistory;
            this.date = this.migrationHistory.date != null ? this.migrationHistory.date.format(DATE_TIME_FORMAT) : null;
        });
        this.migrationService.query().subscribe(
            (res: HttpResponse<IMigration[]>) => {
                this.migrations = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        this.migrationHistory.date = this.date != null ? moment(this.date, DATE_TIME_FORMAT) : null;
        if (this.migrationHistory.id !== undefined) {
            this.subscribeToSaveResponse(this.migrationHistoryService.update(this.migrationHistory));
        } else {
            this.subscribeToSaveResponse(this.migrationHistoryService.create(this.migrationHistory));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<IMigrationHistory>>) {
        result.subscribe((res: HttpResponse<IMigrationHistory>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
    }

    private onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }

    trackMigrationById(index: number, item: IMigration) {
        return item.id;
    }
}
