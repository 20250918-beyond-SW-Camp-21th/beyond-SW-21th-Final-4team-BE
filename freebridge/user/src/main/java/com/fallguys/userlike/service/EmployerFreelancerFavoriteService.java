package com.fallguys.userlike.service;

public interface EmployerFreelancerFavoriteService {

    void addFavorite(Long employerId, Long freelancerId);

    void removeFavorite(Long employerId, Long freelancerId);
}
