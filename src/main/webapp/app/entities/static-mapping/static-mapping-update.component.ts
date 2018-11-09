import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { IStaticMapping } from 'app/shared/model/static-mapping.model';
import { StaticMappingService } from './static-mapping.service';

@Component({
    selector: 'jhi-static-mapping-update',
    templateUrl: './static-mapping-update.component.html'
})
export class StaticMappingUpdateComponent implements OnInit {
    staticMapping: IStaticMapping;
    isSaving: boolean;

    constructor(private staticMappingService: StaticMappingService, private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ staticMapping }) => {
            this.staticMapping = staticMapping;
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.staticMapping.id !== undefined) {
            this.subscribeToSaveResponse(this.staticMappingService.update(this.staticMapping));
        } else {
            this.subscribeToSaveResponse(this.staticMappingService.create(this.staticMapping));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<IStaticMapping>>) {
        result.subscribe((res: HttpResponse<IStaticMapping>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
    }
}
