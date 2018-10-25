import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMigrationHistory } from 'app/shared/model/migration-history.model';
import { MigrationHistoryService } from './migration-history.service';

@Component({
    selector: 'jhi-migration-history-delete-dialog',
    templateUrl: './migration-history-delete-dialog.component.html'
})
export class MigrationHistoryDeleteDialogComponent {
    migrationHistory: IMigrationHistory;

    constructor(
        private migrationHistoryService: MigrationHistoryService,
        public activeModal: NgbActiveModal,
        private eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.migrationHistoryService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'migrationHistoryListModification',
                content: 'Deleted an migrationHistory'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-migration-history-delete-popup',
    template: ''
})
export class MigrationHistoryDeletePopupComponent implements OnInit, OnDestroy {
    private ngbModalRef: NgbModalRef;

    constructor(private activatedRoute: ActivatedRoute, private router: Router, private modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ migrationHistory }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(MigrationHistoryDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.migrationHistory = migrationHistory;
                this.ngbModalRef.result.then(
                    result => {
                        this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                        this.ngbModalRef = null;
                    },
                    reason => {
                        this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
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
