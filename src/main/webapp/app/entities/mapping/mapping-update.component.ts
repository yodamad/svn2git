import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { IMapping } from 'app/shared/model/mapping.model';
import { MappingService } from './mapping.service';

@Component({
    selector: 'jhi-mapping-update',
    templateUrl: './mapping-update.component.html'
})
export class MappingUpdateComponent implements OnInit {
    mapping: IMapping;
    isSaving: boolean;

    constructor(private mappingService: MappingService, private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ mapping }) => {
            this.mapping = mapping;
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.mapping.id !== undefined) {
            this.subscribeToSaveResponse(this.mappingService.update(this.mapping));
        } else {
            this.subscribeToSaveResponse(this.mappingService.create(this.mapping));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<IMapping>>) {
        result.subscribe((res: HttpResponse<IMapping>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
    }
}
