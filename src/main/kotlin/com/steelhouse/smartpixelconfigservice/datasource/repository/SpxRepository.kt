package com.steelhouse.smartpixelconfigservice.datasource.repository

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import org.springframework.data.repository.CrudRepository

interface SpxRepository : CrudRepository<AdvertiserSmartPxVariables, Int>
