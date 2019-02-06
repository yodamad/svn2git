import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IStaticExtension } from 'app/shared/model/static-extension.model';

@Component({
    selector: 'jhi-static-extension-detail',
    templateUrl: './static-extension-detail.component.html'
})
export class StaticExtensionDetailComponent implements OnInit {
    staticExtension: IStaticExtension;

    constructor(private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ staticExtension }) => {
            this.staticExtension = staticExtension;
        });
    }

    previousState() {
        window.history.back();
    }
}
