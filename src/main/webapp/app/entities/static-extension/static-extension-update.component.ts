import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { IStaticExtension } from 'app/shared/model/static-extension.model';
import { StaticExtensionService } from './static-extension.service';

@Component({
    selector: 'jhi-static-extension-update',
    templateUrl: './static-extension-update.component.html'
})
export class StaticExtensionUpdateComponent implements OnInit {
    staticExtension: IStaticExtension;
    isSaving: boolean;

    constructor(private staticExtensionService: StaticExtensionService, private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ staticExtension }) => {
            this.staticExtension = staticExtension;
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.staticExtension.id !== undefined) {
            this.subscribeToSaveResponse(this.staticExtensionService.update(this.staticExtension));
        } else {
            this.subscribeToSaveResponse(this.staticExtensionService.create(this.staticExtension));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<IStaticExtension>>) {
        result.subscribe((res: HttpResponse<IStaticExtension>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
    }
}
