package app.roadrescue.controller;

import app.roadrescue.dto.RequestDtos.CreateRequest;
import app.roadrescue.dto.RequestDtos.ReviewRequest;
import app.roadrescue.dto.RequestDtos.StatusRequest;
import app.roadrescue.model.IssueType;
import app.roadrescue.service.RequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class RequestController extends BaseController {
  private final RequestService requestService;

  public RequestController(RequestService requestService) {
    this.requestService = requestService;
  }

  @GetMapping("/nearby-providers")
  public List<Map<String, Object>> nearbyProviders(
      @RequestParam IssueType issueType,
      @RequestParam double lat,
      @RequestParam double lng
  ) {
    return requestService.nearbyProviders(issueType, lat, lng);
  }

  @GetMapping
  public List<Map<String, Object>> list(HttpServletRequest request) {
    return requestService.list(auth(request));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(HttpServletRequest servletRequest, @Valid @RequestBody CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(auth(servletRequest).userId(), request));
  }

  @GetMapping("/{requestId}")
  public Map<String, Object> get(@PathVariable String requestId) {
    return requestService.get(requestId);
  }

  @PatchMapping("/{requestId}/status")
  public Map<String, Object> updateStatus(HttpServletRequest servletRequest, @PathVariable String requestId, @Valid @RequestBody StatusRequest request) {
    return requestService.updateStatus(auth(servletRequest), requestId, request);
  }

  @PostMapping("/{requestId}/review")
  public ResponseEntity<Map<String, Object>> review(HttpServletRequest servletRequest, @PathVariable String requestId, @Valid @RequestBody ReviewRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(requestService.review(auth(servletRequest), requestId, request));
  }
}
