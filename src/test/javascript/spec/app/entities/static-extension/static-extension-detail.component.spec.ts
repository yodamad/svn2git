/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { StaticExtensionDetailComponent } from 'app/entities/static-extension/static-extension-detail.component';
import { StaticExtension } from 'app/shared/model/static-extension.model';

describe('Component Tests', () => {
    describe('StaticExtension Management Detail Component', () => {
        let comp: StaticExtensionDetailComponent;
        let fixture: ComponentFixture<StaticExtensionDetailComponent>;
        const route = ({ data: of({ staticExtension: new StaticExtension(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [StaticExtensionDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(StaticExtensionDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(StaticExtensionDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.staticExtension).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
