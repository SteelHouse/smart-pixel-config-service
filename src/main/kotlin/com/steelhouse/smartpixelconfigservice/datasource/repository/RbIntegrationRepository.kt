package com.steelhouse.smartpixelconfigservice.datasource.repository

import com.steelhouse.postgresql.publicschema.RockerboxIntegration
import org.springframework.data.repository.CrudRepository

interface RbIntegrationRepository : CrudRepository<RockerboxIntegration, Int>
