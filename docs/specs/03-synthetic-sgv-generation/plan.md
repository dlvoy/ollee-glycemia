# 03: Synthetic SGV generation (plan)

## Approach
A small pipeline in the `synthetic/` package: anchors → normalise → Hermite interpolation → constraints → noise → delta estimation → samples. `SgvGenerationConfig` holds the parameters.

## Files
- `synthetic/SgvAnchor.kt`, `NormalizedAnchor.kt`, `SgvAnchorNormalizer.kt`
- `synthetic/HermiteInterpolator.kt`, `InterpolatedPoint.kt`
- `synthetic/SgvConstraintProcessor.kt`, `SgvNoiseGenerator.kt`, `SgvDeltaEstimator.kt`
- `synthetic/SgvScenarioFactory.kt`, `SgvSyntheticGenerator.kt`, `SgvSample.kt`, `SgvGenerationConfig.kt`

## Steps
1. Anchor model and normalisation
2. Hermite interpolation
3. Constraints and noise
4. Delta estimation
5. Scenario factory and generator facade

## Tests
- `synthetic/SgvSyntheticGeneratorTest`
