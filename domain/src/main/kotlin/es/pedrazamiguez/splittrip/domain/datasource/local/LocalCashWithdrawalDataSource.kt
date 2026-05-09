package es.pedrazamiguez.splittrip.domain.datasource.local

/**
 * Combined marker interface for local cash withdrawal data access.
 *
 * Consumers that require both read and write operations can depend on this type.
 * Prefer the narrower [LocalCashWithdrawalQueryDataSource] or
 * [LocalCashWithdrawalWriteDataSource] interfaces when only one direction is needed.
 */
interface LocalCashWithdrawalDataSource :
    LocalCashWithdrawalQueryDataSource,
    LocalCashWithdrawalWriteDataSource
