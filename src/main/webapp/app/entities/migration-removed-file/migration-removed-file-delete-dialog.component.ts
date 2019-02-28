import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';
import { MigrationRemovedFileService } from './migration-removed-file.service';

@Component({
    selector: 'jhi-migration-removed-file-delete-dialog',
    templateUrl: './migration-removed-file-delete-dialog.component.html'
})
export class MigrationRemovedFileDeleteDialogComponent {
    migrationRemovedFile: IMigrationRemovedFile;

    constructor(
        protected migrationRemovedFileService: MigrationRemovedFileService,
        public activeModal: NgbActiveModal,
        protected eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.migrationRemovedFileService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'migrationRemovedFileListModification',
                content: 'Deleted an migrationRemovedFile'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-migration-removed-file-delete-popup',
    template: ''
})
export class MigrationRemovedFileDeletePopupComponent implements OnInit, OnDestroy {
    protected ngbModalRef: NgbModalRef;

    constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ migrationRemovedFile }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(MigrationRemovedFileDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.migrationRemovedFile = migrationRemovedFile;
                this.ngbModalRef.result.then(
                    result => {
                        this.router.navigate(['/migration-removed-file', { outlets: { popup: null } }]);
                        this.ngbModalRef = null;
                    },
                    reason => {
                        this.router.navigate(['/migration-removed-file', { outlets: { popup: null } }]);
                        this.ngbModalRef = null;
                    }
                );
            }, 0);
        });
    }

    ngOnDestroy() {
        this.ngbModalRef = null;
    }
}
