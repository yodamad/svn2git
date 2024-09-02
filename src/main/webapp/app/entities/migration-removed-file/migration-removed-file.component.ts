import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiAlertService } from 'ng-jhipster';
import { IMigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';
import { MigrationRemovedFileService } from './migration-removed-file.service';

@Component({
    selector: 'jhi-migration-removed-file',
    templateUrl: './migration-removed-file.component.html'
})
export class MigrationRemovedFileComponent implements OnInit, OnDestroy {
    migrationRemovedFiles: IMigrationRemovedFile[];
    currentAccount: any;
    eventSubscriber: Subscription;

    constructor(
        private migrationRemovedFileService: MigrationRemovedFileService,
        private jhiAlertService: JhiAlertService,
        private eventManager: JhiEventManager
    ) {}

    loadAll() {
        this.migrationRemovedFileService.query().subscribe(
            (res: HttpResponse<IMigrationRemovedFile[]>) => {
                this.migrationRemovedFiles = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    ngOnInit() {
        this.loadAll();
        this.registerChangeInMigrationRemovedFiles();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: IMigrationRemovedFile) {
        return item.id;
    }

    registerChangeInMigrationRemovedFiles() {
        this.eventSubscriber = this.eventManager.subscribe('migrationRemovedFileListModification', response => this.loadAll());
    }

    private onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
