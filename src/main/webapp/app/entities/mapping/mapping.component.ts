import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiAlertService } from 'ng-jhipster';
import { IMapping } from 'app/shared/model/mapping.model';
import { MappingService } from './mapping.service';

@Component({
    selector: 'jhi-mapping',
    templateUrl: './mapping.component.html'
})
export class MappingComponent implements OnInit, OnDestroy {
    mappings: IMapping[];
    currentAccount: any;
    eventSubscriber: Subscription;

    constructor(private mappingService: MappingService, private jhiAlertService: JhiAlertService, private eventManager: JhiEventManager) {}

    loadAll() {
        this.mappingService.query().subscribe(
            (res: HttpResponse<IMapping[]>) => {
                this.mappings = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    ngOnInit() {
        this.loadAll();
        this.registerChangeInMappings();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: IMapping) {
        return item.id;
    }

    registerChangeInMappings() {
        this.eventSubscriber = this.eventManager.subscribe('mappingListModification', response => this.loadAll());
    }

    private onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
