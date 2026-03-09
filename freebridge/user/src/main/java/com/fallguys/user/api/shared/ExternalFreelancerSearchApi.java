package com.fallguys.user.api.shared;

import com.fallguys.user.api.shared.response.ExternalFreelancerSearchResponse;

public interface ExternalFreelancerSearchApi {

    ExternalFreelancerSearchResponse searchFreelancers(int page, int size, String keyword);
}
