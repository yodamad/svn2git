/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { Svn2GitTestModule } from '../../../test.module';
import { MappingComponent } from 'app/entities/mapping/mapping.component';
import { MappingService } from 'app/entities/mapping/mapping.service';
import { Mapping } from 'app/shared/model/mapping.model';

describe('Component Tests', () => {
    describe('Mapping Management Component', () => {
        let comp: MappingComponent;
        let fixture: ComponentFixture<MappingComponent>;
        let service: MappingService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MappingComponent],
                providers: []
            })
                .overrideTemplate(MappingComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(MappingComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MappingService);
        });

        it('Should call load all on init', () => {
            // GIVEN
            const headers = new HttpHeaders().append('link', 'link;link');
            spyOn(service, 'query').and.returnValue(
                of(
                    new HttpResponse({
                        body: [new Mapping(123)],
                        headers
                    })
                )
            );

            // WHEN
            comp.ngOnInit();

            // THEN
            expect(service.query).toHaveBeenCalled();
            expect(comp.mappings[0]).toEqual(jasmine.objectContaining({ id: 123 }));
        });
    });
});
