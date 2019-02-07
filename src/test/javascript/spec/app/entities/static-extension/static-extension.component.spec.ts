/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { Svn2GitTestModule } from '../../../test.module';
import { StaticExtensionComponent } from 'app/entities/static-extension/static-extension.component';
import { StaticExtensionService } from 'app/entities/static-extension/static-extension.service';
import { StaticExtension } from 'app/shared/model/static-extension.model';

describe('Component Tests', () => {
    describe('StaticExtension Management Component', () => {
        let comp: StaticExtensionComponent;
        let fixture: ComponentFixture<StaticExtensionComponent>;
        let service: StaticExtensionService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [StaticExtensionComponent],
                providers: []
            })
                .overrideTemplate(StaticExtensionComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(StaticExtensionComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(StaticExtensionService);
        });

        it('Should call load all on init', () => {
            // GIVEN
            const headers = new HttpHeaders().append('link', 'link;link');
            spyOn(service, 'query').and.returnValue(
                of(
                    new HttpResponse({
                        body: [new StaticExtension(123)],
                        headers
                    })
                )
            );

            // WHEN
            comp.ngOnInit();

            // THEN
            expect(service.query).toHaveBeenCalled();
            expect(comp.staticExtensions[0]).toEqual(jasmine.objectContaining({ id: 123 }));
        });
    });
});
