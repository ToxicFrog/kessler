namespace FundingFloor {
  internal class Settings : GameParameters.CustomParameterNode {
    public override string Title => "Funding Floor";
    public override string Section => Title;
    public override string DisplaySection => Section;
    public override int SectionOrder => 1;
    public override GameParameters.GameMode GameMode => GameParameters.GameMode.CAREER;
    public override bool HasPresets => false;

    [GameParameters.CustomIntParameterUI("Min funding",
      toolTip = "How much you can spend at 0 (or less) reputation.",
      minValue = 10_000, maxValue = 100_000, stepSize = 1000)]
    public int MinFunding = 10_000;

    [GameParameters.CustomIntParameterUI("Max funding",
      toolTip = "How much you can spend at 1000 reputation.\nRecommended at least 2M so you can buy all the upgrades.",
      minValue = 500_000, maxValue = 10_000_000, stepSize = 100_00)]
    public int MaxFunding = 2_000_000;

    // Conversion ratios:
    // Ongoing TO funds: 2500 F : 2 rep : 1 sci
    // Ongoing FROM funds: 10k F : 2 rep : 1 sci
    // Immediate TO funds: 100 F : 1 rep : 2 sci
    [GameParameters.CustomIntParameterUI("R&D min funding bonus",
      toolTip = "How much min funding increases per science spent.\nOnly science spent on R&D unlocks counts.",
      minValue = 0, maxValue = 100, stepSize = 5)]
    public int MinFundingPerScience = 5;

    [GameParameters.CustomIntParameterUI("R&D max funding bonus",
      toolTip = "How much max funding increases per science spent.\nOnly science spent on R&D unlocks counts.",
      minValue = 0, maxValue = 100, stepSize = 5)]
    public int MaxFundingPerScience = 50;

    [GameParameters.CustomFloatParameterUI("Funding penalty for expenses",
      toolTip = "Spending money slightly decreases reputation.",
      minValue = 0.0f, maxValue = 0.1f, asPercentage = true, displayFormat = "N3")]
    public float FundingPenaltyPercent = 0;

    [GameParameters.CustomFloatParameterUI("Convert income to funding",
      toolTip = "Earning money slightly increases reputation.",
      minValue = 0.0f, maxValue = 0.1f, asPercentage = true, displayFormat = "N3")]
    public float FundingBonusPercent = 0;

    [GameParameters.CustomFloatParameterUI("Convert income to science",
      toolTip = "Earning money gives a small amount of science.",
      minValue = 0.0f, maxValue = 0.1f, asPercentage = true, displayFormat = "N3")]
    public float ScienceBonusPercent = 0;
  }
}
