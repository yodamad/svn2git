import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IStaticExtension } from 'app/shared/model/static-extension.model';
import { StaticExtensionService } from './static-extension.service';

@Component({
    selector: 'jhi-static-extension-delete-dialog',
    templateUrl: './static-extension-delete-dialog.component.html'
})
export class StaticExtensionDeleteDialogComponent {
    staticExtension: IStaticExtension;

    constructor(
        private staticExtensionService: StaticExtensionService,
        public activeModal: NgbActiveModal,
        private eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.staticExtensionService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'staticExtensionListModification',
                content: 'Deleted an staticExtension'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-static-extension-delete-popup',
    template: ''
})
export class StaticExtensionDeletePopupComponent implements OnInit, OnDestroy {
    private ngbModalRef: NgbModalRef;

    constructor(private activatedRoute: ActivatedRoute, private router: Router, private modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ staticExtension }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(StaticExtensionDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.staticExtension = staticExtension;
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
