import { TestBed } from "@angular/core/testing";
import {
  HttpClientTestingModule,
  HttpTestingController,
} from "@angular/common/http/testing";
import { environment } from "../../environments/environment";
import { DisputeService } from "./dispute.service";

describe("DisputeService", () => {
  let service: DisputeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(DisputeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("posts only the order ID and description to the existing dispute endpoint", () => {
    const result = { id: 12, tradeOrderId: 7, status: "OPEN" };
    service
      .create({ tradeOrderId: 7, message: "Incorrect Price" })
      .subscribe((response) => {
        expect(response.id).toBe(12);
      });
    const request = http.expectOne(`${environment.apiUrl}/dispute`);
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual({
      tradeOrderId: 7,
      message: "Incorrect Price",
    });
    request.flush(result, { status: 201, statusText: "Created" });
  });

  it("propagates conflicts without retrying the creation", () => {
    let status = 0;
    service
      .create({ tradeOrderId: 7, message: "Wrong price" })
      .subscribe({ error: (error) => (status = error.status) });
    http
      .expectOne(`${environment.apiUrl}/dispute`)
      .flush({}, { status: 409, statusText: "Conflict" });
    expect(status).toBe(409);
    http.expectNone(`${environment.apiUrl}/dispute`);
  });

  it("resolves the selected dispute with a PATCH body and returns the saved text", () => {
    const saved = jasmine.createSpy("saved");
    service.resolve(7, "Reviewed and corrected").subscribe(saved);
    const request = http.expectOne(`${environment.apiUrl}/dispute/7/resolve`);
    expect(request.request.method).toBe("PATCH");
    expect(request.request.body).toEqual({
      resolutionMessage: "Reviewed and corrected",
    });
    request.flush({
      id: 7,
      status: "RESOLVED",
      resolutionMessage: "Reviewed and corrected",
    });
    expect(saved).toHaveBeenCalledWith(
      jasmine.objectContaining({ resolutionMessage: "Reviewed and corrected" }),
    );
  });

  it("surfaces resolution failures instead of returning a success result", () => {
    const success = jasmine.createSpy("success");
    const failure = jasmine.createSpy("failure");
    service.resolve(7, "Reviewed").subscribe({ next: success, error: failure });
    http
      .expectOne(`${environment.apiUrl}/dispute/7/resolve`)
      .flush({}, { status: 403, statusText: "Forbidden" });
    expect(success).not.toHaveBeenCalled();
    expect(failure).toHaveBeenCalled();
  });

  it("sends every supported query parameter without losing zeros or decimal strings", () => {
    service
      .search({
        page: 0,
        size: 100,
        sortBy: "tradeValue",
        sortDirection: "asc",
        accountId: "12",
        fromDate: "2026-10-22T00:00",
        toDate: "2026-10-23T00:00",
        minTradeValue: "0",
        maxTradeValue: "12345.67",
        status: "OPEN",
      })
      .subscribe();
    const request = http.expectOne(
      (req) => req.url === `${environment.apiUrl}/dispute/search`,
    );
    expect(request.request.method).toBe("GET");
    expect(request.request.params.get("page")).toBe("0");
    expect(request.request.params.get("size")).toBe("100");
    expect(request.request.params.get("sortBy")).toBe("tradeValue");
    expect(request.request.params.get("status")).toBe("OPEN");
    expect(request.request.params.get("sortDirection")).toBe("asc");
    expect(request.request.params.get("accountId")).toBe("12");
    expect(request.request.params.get("fromDate")).toBe("2026-10-22T00:00");
    expect(request.request.params.get("toDate")).toBe("2026-10-23T00:00");
    expect(request.request.params.get("minTradeValue")).toBe("0");
    expect(request.request.params.get("maxTradeValue")).toBe("12345.67");
    request.flush({
      content: [],
      number: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    });
  });

  it("omits cleared optional filters and surfaces failures", () => {
    let failed = false;
    service
      .search({
        page: 0,
        size: 20,
        sortBy: "createdAt",
        sortDirection: "desc",
        accountId: "",
        status: "",
      })
      .subscribe({ error: () => (failed = true) });
    const request = http.expectOne((req) =>
      req.url.endsWith("/dispute/search"),
    );
    expect(request.request.params.has("accountId")).toBeFalse();
    expect(request.request.params.has("status")).toBeFalse();
    expect(request.request.params.has("fromDate")).toBeFalse();
    request.flush({}, { status: 500, statusText: "Error" });
    expect(failed).toBeTrue();
  });

  it("requests all statuses for the selected user with server-side open-first ordering", () => {
    service
      .search({
        page: 0,
        size: 20,
        sortBy: "status",
        sortDirection: "asc",
        userId: 42,
      })
      .subscribe();
    const request = http.expectOne((req) =>
      req.url.endsWith("/dispute/search"),
    );
    expect(request.request.params.get("userId")).toBe("42");
    expect(request.request.params.get("sortBy")).toBe("status");
    expect(request.request.params.get("sortDirection")).toBe("asc");
    expect(request.request.params.has("status")).toBeFalse();
    expect(request.request.params.has("accountId")).toBeFalse();
    request.flush({
      content: [],
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });
});
