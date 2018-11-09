import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IStaticMapping } from 'app/shared/model/static-mapping.model';

@Component({
    selector: 'jhi-static-mapping-detail',
    templateUrl: './static-mapping-detail.component.html'
})
export class StaticMappingDetailComponent implements OnInit {
    staticMapping: IStaticMapping;

    constructor(private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ staticMapping }) => {
            this.staticMapping = staticMapping;
        });
    }

    previousState() {
        window.history.back();
    }
}
