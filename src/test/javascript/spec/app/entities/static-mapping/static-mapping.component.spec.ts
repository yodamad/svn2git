/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { Svn2GitTestModule } from '../../../test.module';
import { StaticMappingComponent } from 'app/entities/static-mapping/static-mapping.component';
import { StaticMappingService } from 'app/entities/static-mapping/static-mapping.service';
import { StaticMapping } from 'app/shared/model/static-mapping.model';

describe('Component Tests', () => {
    describe('StaticMapping Management Component', () => {
        let comp: StaticMappingComponent;
        let fixture: ComponentFixture<StaticMappingComponent>;
        let service: StaticMappingService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [StaticMappingComponent],
                providers: []
            })
                .overrideTemplate(StaticMappingComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(StaticMappingComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(StaticMappingService);
        });

        it('Should call load all on init', () => {
            // GIVEN
            const headers = new HttpHeaders().append('link', 'link;link');
            spyOn(service, 'query').and.returnValue(
                of(
                    new HttpResponse({
                        body: [new StaticMapping(123)],
                        headers
                    })
                )
            );

            // WHEN
            comp.ngOnInit();

            // THEN
            expect(service.query).toHaveBeenCalled();
            expect(comp.staticMappings[0]).toEqual(jasmine.objectContaining({ id: 123 }));
        });
    });
});
