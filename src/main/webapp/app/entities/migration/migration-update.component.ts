import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';

import { IMigration } from 'app/shared/model/migration.model';
import { MigrationService } from './migration.service';

@Component({
    selector: 'jhi-migration-update',
    templateUrl: './migration-update.component.html'
})
export class MigrationUpdateComponent implements OnInit {
    migration: IMigration;
    isSaving: boolean;
    dateDp: any;
    createdTimestamp: string;

    constructor(private migrationService: MigrationService, private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ migration }) => {
            this.migration = migration;
            this.createdTimestamp =
                this.migration.createdTimestamp != null ? this.migration.createdTimestamp.format(DATE_TIME_FORMAT) : null;
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        this.migration.createdTimestamp = this.createdTimestamp != null ? moment(this.createdTimestamp, DATE_TIME_FORMAT) : null;
        if (this.migration.id !== undefined) {
            this.subscribeToSaveResponse(this.migrationService.update(this.migration));
        } else {
            this.subscribeToSaveResponse(this.migrationService.create(this.migration));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<IMigration>>) {
        result.subscribe((res: HttpResponse<IMigration>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
    }
}
