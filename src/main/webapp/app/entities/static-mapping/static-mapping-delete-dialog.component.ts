import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IStaticMapping } from 'app/shared/model/static-mapping.model';
import { StaticMappingService } from './static-mapping.service';

@Component({
    selector: 'jhi-static-mapping-delete-dialog',
    templateUrl: './static-mapping-delete-dialog.component.html'
})
export class StaticMappingDeleteDialogComponent {
    staticMapping: IStaticMapping;

    constructor(
        private staticMappingService: StaticMappingService,
        public activeModal: NgbActiveModal,
        private eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.staticMappingService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'staticMappingListModification',
                content: 'Deleted an staticMapping'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-static-mapping-delete-popup',
    template: ''
})
export class StaticMappingDeletePopupComponent implements OnInit, OnDestroy {
    private ngbModalRef: NgbModalRef;

    constructor(private activatedRoute: ActivatedRoute, private router: Router, private modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ staticMapping }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(StaticMappingDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.staticMapping = staticMapping;
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
