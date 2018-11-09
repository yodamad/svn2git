/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { StaticMappingDetailComponent } from 'app/entities/static-mapping/static-mapping-detail.component';
import { StaticMapping } from 'app/shared/model/static-mapping.model';

describe('Component Tests', () => {
    describe('StaticMapping Management Detail Component', () => {
        let comp: StaticMappingDetailComponent;
        let fixture: ComponentFixture<StaticMappingDetailComponent>;
        const route = ({ data: of({ staticMapping: new StaticMapping(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [StaticMappingDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(StaticMappingDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(StaticMappingDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.staticMapping).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
