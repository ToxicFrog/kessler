namespace FundingFloor {
  internal class Settings : GameParameters.CustomParameterNode {
    public override string Title => "Funding Floor";
    public override string Section => Title;
    public override string DisplaySection => Section;
    public override int SectionOrder => 1;
    public override GameParameters.GameMode GameMode => GameParameters.GameMode.ANY;
    public override bool HasPresets => false;

    [GameParameters.CustomIntParameterUI("Min funding",
      toolTip = "How much you can spend at 0 (or less) reputation.",
      minValue = 10_000, maxValue = 100_000, stepSize = 1000)]
    public int MinFunding = 10000;

    [GameParameters.CustomIntParameterUI("Max funding",
      toolTip = "How much you can spend at 1000 reputation.\nRecommended at least 2M so you can buy all the upgrades.",
      minValue = 500_000, maxValue = 10_000_000, stepSize = 100_00)]
    public int MaxFunding = 2_000_000;

    // Conversion ratios:
    // Ongoing TO funds: 2500 F : 2 rep : 1 sci
    // Ongoing FROM funds: 10k F : 2 rep : 1 sci
    // Immediate TO funds: 100 F : 1 rep : 2 sci
    [GameParameters.CustomIntParameterUI("Bonus funding per science",
      toolTip = "How much min funding increases per science spent.\nOnly science spent on R&D unlocks counts.",
      minValue = 0, maxValue = 100, stepSize = 5)]
    public int FundingPerScience = 50;

    [GameParameters.CustomFloatParameterUI("Funding decay per transaction (%)",
      toolTip = "Spending money slightly decreases reputation.\nIf 'keep excess' is on, spending excess doesn't count.",
      minValue = 0.0f, maxValue = 10.0f, stepCount = 100)]
    public float FundingDecayPercent = 0;

    [GameParameters.CustomParameterUI("Keep excess funding",
      toolTip = "Funding earned in excess of what your reputation would give you (from recovery, contracts, etc) can be spent normally, temporarily increasing your budget.")]
    public bool KeepExcessFunding = true;

    // [GameParameters.CustomFloatParameterUI("Excess funding conversion to science",
    //   toolTip = "Excess funding earned will be converted into science at this ratio.",
    //   minValue = 0, maxValue = 0.1f, stepCount = 100)]
    // public float ExcessScienceConversion = 0;

    // FIXME: convert into reputation based on the amount of reputation needed to get that amount of funding,
    // rather than based on a straight conversion? I.e. if you gain 10k funds, and this ratio is set to 0.1,
    // and your reputation-to-funding ratio is set to 500, you would gain 2 reputation instead of 1000.
    // [GameParameters.CustomFloatParameterUI("Excess funding conversion to reputation",
    //   toolTip = "Excess funding earned will be converted into reputation at this ratio.",
    //   minValue = 0, maxValue = 0.1f, stepCount = 100)]
    // public float ExcessReputationConversion = 0;
  }
}
