import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiAlertService } from 'ng-jhipster';
import { IMigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';
import { MigrationRemovedFileService } from './migration-removed-file.service';
import { IMigration } from 'app/shared/model/migration.model';
import { MigrationService } from 'app/entities/migration';

@Component({
    selector: 'jhi-migration-removed-file-update',
    templateUrl: './migration-removed-file-update.component.html'
})
export class MigrationRemovedFileUpdateComponent implements OnInit {
    migrationRemovedFile: IMigrationRemovedFile;
    isSaving: boolean;

    migrations: IMigration[];

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected migrationRemovedFileService: MigrationRemovedFileService,
        protected migrationService: MigrationService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ migrationRemovedFile }) => {
            this.migrationRemovedFile = migrationRemovedFile;
        });
        this.migrationService
            .query()
            .pipe(
                filter((mayBeOk: HttpResponse<IMigration[]>) => mayBeOk.ok),
                map((response: HttpResponse<IMigration[]>) => response.body)
            )
            .subscribe((res: IMigration[]) => (this.migrations = res), (res: HttpErrorResponse) => this.onError(res.message));
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.migrationRemovedFile.id !== undefined) {
            this.subscribeToSaveResponse(this.migrationRemovedFileService.update(this.migrationRemovedFile));
        } else {
            this.subscribeToSaveResponse(this.migrationRemovedFileService.create(this.migrationRemovedFile));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<IMigrationRemovedFile>>) {
        result.subscribe(
            (res: HttpResponse<IMigrationRemovedFile>) => this.onSaveSuccess(),
            (res: HttpErrorResponse) => this.onSaveError()
        );
    }

    protected onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    protected onSaveError() {
        this.isSaving = false;
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }

    trackMigrationById(index: number, item: IMigration) {
        return item.id;
    }
}
